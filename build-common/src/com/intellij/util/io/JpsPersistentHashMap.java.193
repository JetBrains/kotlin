// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.io;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.ThreadLocalCachedValue;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.*;
import com.intellij.util.containers.LimitedPool;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class shouldn't be used. It's temporary solution for JSP only and should be removed
 * after updating Intellij SDK version
 */
@Deprecated
public class JpsPersistentHashMap<Key, Value> extends PersistentEnumeratorDelegate<Key> implements PersistentMap<Key, Value> {
    // PersistentHashMap (PHM) works in the following (generic) way:
    // - Particular key is translated via myEnumerator into an int.
    // - As part of enumeration process for the new key, additional space is reserved in
    // myEnumerator.myStorage for offset in ".values" file (myValueStorage) where (serialized) value is stored.
    // - Once new value is written the offset storage is updated.
    // - When the key is removed from PHM, offset storage is set to zero.
    //
    // It is important to note that offset
    // is non-negative and can be 4 or 8 bytes, depending on the size of the ".values" file.
    // PHM can work in appendable mode: for particular key additional calculated chunk of value can be appended to ".values" file with the offset
    // of previously calculated chunk.
    // For performance reasons we try hard to minimize storage occupied by keys / offsets in ".values" file: this storage is allocated as (limited)
    // direct byte buffers so 4 bytes offset is used until it is possible. Generic record produced by enumerator used with PHM as part of new
    // key enumeration is <enumerated_id>? [.values file offset 4 or 8 bytes], however for unique integral keys enumerate_id isn't produced.
    // Also for certain Value types it is possible to avoid random reads at all: e.g. in case Value is non-negative integer the value can be stored
    // directly in storage used for offset and in case of btree enumerator directly in btree leaf.
    private static final Logger LOG = Logger.getInstance("#com.intellij.util.io.PersistentHashMap");
    private static final boolean myDoTrace = SystemProperties.getBooleanProperty("idea.trace.persistent.map", false);
    private static final int DEAD_KEY_NUMBER_MASK = 0xFFFFFFFF;

    private final File myStorageFile;
    private final boolean myIsReadOnly;
    private final KeyDescriptor<Key> myKeyDescriptor;
    private PersistentHashMapValueStorage myValueStorage;
    protected final DataExternalizer<Value> myValueExternalizer;
    private static final long NULL_ADDR = 0;
    private static final int INITIAL_INDEX_SIZE;

    static {
        String property = System.getProperty("idea.initialIndexSize");
        INITIAL_INDEX_SIZE = property == null ? 4 * 1024 : Integer.valueOf(property);
    }

    @NonNls
    static final String DATA_FILE_EXTENSION = ".values";
    private long myLiveAndGarbageKeysCounter;
    // first four bytes contain live keys count (updated via LIVE_KEY_MASK), last four bytes - number of dead keys
    private int myReadCompactionGarbageSize;
    private static final long LIVE_KEY_MASK = 1L << 32;
    private static final long USED_LONG_VALUE_MASK = 1L << 62;
    private static final int POSITIVE_VALUE_SHIFT = 1;
    private final int myParentValueRefOffset;
    @NotNull private final byte[] myRecordBuffer;
    @NotNull private final byte[] mySmallRecordBuffer;
    private final boolean myIntMapping;
    private final boolean myDirectlyStoreLongFileOffsetMode;
    private final boolean myCanReEnumerate;
    private int myLargeIndexWatermarkId;  // starting with this id we store offset in adjacent file in long format
    private boolean myIntAddressForNewRecord;
    private static final boolean doHardConsistencyChecks = false;
    private volatile boolean myBusyReading;

    private static class AppendStream extends DataOutputStream {
        private AppendStream() {
            super(null);
        }

        private void setOut(BufferExposingByteArrayOutputStream stream) {
            out = stream;
        }
    }

    private final LimitedPool<BufferExposingByteArrayOutputStream> myStreamPool =
            new LimitedPool<>(10, new LimitedPool.ObjectFactory<BufferExposingByteArrayOutputStream>() {
                @Override
                @NotNull
                public BufferExposingByteArrayOutputStream create() {
                    return new BufferExposingByteArrayOutputStream();
                }

                @Override
                public void cleanup(@NotNull final BufferExposingByteArrayOutputStream appendStream) {
                    appendStream.reset();
                }
            });

    private final SLRUCache<Key, BufferExposingByteArrayOutputStream> myAppendCache;

    private boolean canUseIntAddressForNewRecord(long size) {
        return myCanReEnumerate && size + POSITIVE_VALUE_SHIFT < Integer.MAX_VALUE;
    }

    private final LowMemoryWatcher myAppendCacheFlusher = LowMemoryWatcher.register(this::dropMemoryCaches);

    public JpsPersistentHashMap(@NotNull final File file,
            @NotNull KeyDescriptor<Key> keyDescriptor,
            @NotNull DataExternalizer<Value> valueExternalizer) throws IOException {
        this(file, keyDescriptor, valueExternalizer, INITIAL_INDEX_SIZE);
    }

    public JpsPersistentHashMap(@NotNull final File file,
            @NotNull KeyDescriptor<Key> keyDescriptor,
            @NotNull DataExternalizer<Value> valueExternalizer,
            final int initialSize) throws IOException {
        this(file, keyDescriptor, valueExternalizer, initialSize, 0);
    }

    public JpsPersistentHashMap(@NotNull final File file,
            @NotNull KeyDescriptor<Key> keyDescriptor,
            @NotNull DataExternalizer<Value> valueExternalizer,
            final int initialSize,
            int version) throws IOException {
        this(file, keyDescriptor, valueExternalizer, initialSize, version, null);
    }

    public JpsPersistentHashMap(@NotNull final File file,
            @NotNull KeyDescriptor<Key> keyDescriptor,
            @NotNull DataExternalizer<Value> valueExternalizer,
            final int initialSize,
            int version,
            @Nullable PagedFileStorage.StorageLockContext lockContext) throws IOException {
        this(file, keyDescriptor, valueExternalizer, initialSize, version, lockContext,
             PersistentHashMapValueStorage.CreationTimeOptions.threadLocalOptions());
    }

    private JpsPersistentHashMap(@NotNull final File file,
            @NotNull KeyDescriptor<Key> keyDescriptor,
            @NotNull DataExternalizer<Value> valueExternalizer,
            final int initialSize,
            int version,
            @Nullable PagedFileStorage.StorageLockContext lockContext,
            @NotNull PersistentHashMapValueStorage.CreationTimeOptions options) throws IOException {
        super(checkDataFiles(file), keyDescriptor, initialSize, lockContext, modifyVersionDependingOnOptions(version, options));

        myStorageFile = file;
        myKeyDescriptor = keyDescriptor;
        myIsReadOnly = isReadOnly();
        if (myIsReadOnly) options = options.setReadOnly();

        myAppendCache = createAppendCache(keyDescriptor);
        final PersistentEnumeratorBase.RecordBufferHandler<PersistentEnumeratorBase> recordHandler = myEnumerator.getRecordHandler();
        myParentValueRefOffset = recordHandler.getRecordBuffer(myEnumerator).length;
        myIntMapping = valueExternalizer instanceof IntInlineKeyDescriptor && wantNonNegativeIntegralValues();
        myDirectlyStoreLongFileOffsetMode = keyDescriptor instanceof InlineKeyDescriptor && myEnumerator instanceof PersistentBTreeEnumerator;

        myRecordBuffer = myDirectlyStoreLongFileOffsetMode ? ArrayUtilRt.EMPTY_BYTE_ARRAY : new byte[myParentValueRefOffset + 8];
        mySmallRecordBuffer = myDirectlyStoreLongFileOffsetMode ? ArrayUtilRt.EMPTY_BYTE_ARRAY : new byte[myParentValueRefOffset + 4];

        myEnumerator.setRecordHandler(new PersistentEnumeratorBase.RecordBufferHandler<PersistentEnumeratorBase>() {
            @Override
            int recordWriteOffset(PersistentEnumeratorBase enumerator, byte[] buf) {
                return recordHandler.recordWriteOffset(enumerator, buf);
            }

            @NotNull
            @Override
            byte[] getRecordBuffer(PersistentEnumeratorBase enumerator) {
                return myIntAddressForNewRecord ? mySmallRecordBuffer : myRecordBuffer;
            }

            @Override
            void setupRecord(PersistentEnumeratorBase enumerator, int hashCode, int dataOffset, @NotNull byte[] buf) {
                recordHandler.setupRecord(enumerator, hashCode, dataOffset, buf);
                for (int i = myParentValueRefOffset; i < buf.length; i++) {
                    buf[i] = 0;
                }
            }
        });

        myEnumerator.setMarkCleanCallback(
                new Flushable() {
                    @Override
                    public void flush() {
                        myEnumerator.putMetaData(myLiveAndGarbageKeysCounter);
                        myEnumerator.putMetaData2(myLargeIndexWatermarkId | ((long)myReadCompactionGarbageSize << 32));
                    }
                }
        );

        if (myDoTrace) LOG.info("Opened " + file);
        try {
            myValueExternalizer = valueExternalizer;
            myValueStorage = PersistentHashMapValueStorage.create(getDataFile(file).getPath(), options);
            myLiveAndGarbageKeysCounter = myEnumerator.getMetaData();
            long data2 = myEnumerator.getMetaData2();
            myLargeIndexWatermarkId = (int)(data2 & DEAD_KEY_NUMBER_MASK);
            myReadCompactionGarbageSize = (int)(data2 >>> 32);
            myCanReEnumerate = myEnumerator.canReEnumerate();

            if (makesSenseToCompact()) {
                compact();
            }
        }
        catch (IOException e) {
            try {
                // attempt to close already opened resources
                close();
            }
            catch (Throwable ignored) {
            }
            throw e; // rethrow
        }
        catch (Throwable t) {
            LOG.error(t);
            try {
                // attempt to close already opened resources
                close();
            }
            catch (Throwable ignored) {
            }
            throw new PersistentEnumerator.CorruptedException(file);
        }
    }

    private static int modifyVersionDependingOnOptions(int version, @NotNull PersistentHashMapValueStorage.CreationTimeOptions options) {
        return version + options.getVersion();
    }

    protected boolean wantNonNegativeIntegralValues() {
        return false;
    }

    protected boolean isReadOnly() {
        return false;
    }

    private static final int MAX_RECYCLED_BUFFER_SIZE = 4096;

    private SLRUCache<Key, BufferExposingByteArrayOutputStream> createAppendCache(final KeyDescriptor<Key> keyDescriptor) {
        return new SLRUCache<Key, BufferExposingByteArrayOutputStream>(16 * 1024, 4 * 1024, keyDescriptor) {
            @Override
            @NotNull
            public BufferExposingByteArrayOutputStream createValue(final Key key) {
                return myStreamPool.alloc();
            }

            @Override
            protected void onDropFromCache(final Key key, @NotNull final BufferExposingByteArrayOutputStream bytes) {
                appendDataWithoutCache(key, bytes);
            }
        };
    }

    private static boolean doNewCompact() {
        return System.getProperty("idea.persistent.hash.map.oldcompact") == null;
    }

    private boolean forceNewCompact() {
        return System.getProperty("idea.persistent.hash.map.newcompact") != null &&
               (int)(myLiveAndGarbageKeysCounter & DEAD_KEY_NUMBER_MASK) > 0;
    }

    public final void dropMemoryCaches() {
        if (myDoTrace) LOG.info("Drop memory caches " + myStorageFile);
        synchronized (myEnumerator) {
            doDropMemoryCaches();
        }
    }

    protected void doDropMemoryCaches() {
        myEnumerator.lockStorage();
        try {
            clearAppenderCaches();
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    int getGarbageSize() {
        return (int)myLiveAndGarbageKeysCounter;
    }

    public File getBaseFile() {
        return myEnumerator.myFile;
    }

    @TestOnly // public for tests
    @SuppressWarnings("WeakerAccess") // used in upsource for some reason
    public boolean makesSenseToCompact() {
        if (myIsReadOnly) return false;

        final long fileSize = myValueStorage.getSize();
        final int megabyte = 1024 * 1024;

        if (fileSize > 5 * megabyte) { // file is longer than 5MB and (more than 50% of keys is garbage or approximate benefit larger than 100M)
            int liveKeys = (int)(myLiveAndGarbageKeysCounter / LIVE_KEY_MASK);
            int deadKeys = (int)(myLiveAndGarbageKeysCounter & DEAD_KEY_NUMBER_MASK);

            if (fileSize > 50 * megabyte && forceNewCompact()) return true;
            if (deadKeys < 50) return false;

            final long benefitSize = Math.max(100 * megabyte, fileSize / 4);
            final long avgValueSize = fileSize / (liveKeys + deadKeys);

            return deadKeys > liveKeys ||
                   avgValueSize * deadKeys > benefitSize ||
                   myReadCompactionGarbageSize > fileSize / 2;
        }
        return false;
    }

    @NotNull
    private static File checkDataFiles(@NotNull final File file) {
        if (!file.exists()) {
            deleteFilesStartingWith(getDataFile(file));
        }
        return file;
    }

    public static void deleteFilesStartingWith(@NotNull File prefixFile) {
        IOUtil.deleteAllFilesStartingWith(prefixFile);
    }

    @NotNull
    static File getDataFile(@NotNull final File file) { // made public for testing
        return new File(file.getParentFile(), file.getName() + DATA_FILE_EXTENSION);
    }

    @Override
    public final void put(Key key, Value value) throws IOException {
        if (myIsReadOnly) throw new IncorrectOperationException();
        synchronized (myEnumerator) {
            try {
                doPut(key, value);
            }
            catch (IOException ex) {
                myEnumerator.markCorrupted();
                throw ex;
            }
        }
    }

    protected void doPut(Key key, Value value) throws IOException {
        long newValueOffset = -1;

        if (!myIntMapping) {
            final BufferExposingByteArrayOutputStream bytes = new BufferExposingByteArrayOutputStream();
            AppendStream appenderStream = ourFlyweightAppenderStream.getValue();
            appenderStream.setOut(bytes);
            myValueExternalizer.save(appenderStream, value);
            appenderStream.setOut(null);
            newValueOffset = myValueStorage.appendBytes(bytes.toByteArraySequence(), 0);
        }

        myEnumerator.lockStorage();
        try {
            myEnumerator.markDirty(true);
            myAppendCache.remove(key);

            long oldValueOffset;
            if (myDirectlyStoreLongFileOffsetMode) {
                if (myIntMapping) {
                    ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(key, (Integer)value);
                    return;
                }
                oldValueOffset = ((PersistentBTreeEnumerator<Key>)myEnumerator).getNonNegativeValue(key);
                ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(key, newValueOffset);
            }
            else {
                final int id = enumerate(key);
                if (myIntMapping) {
                    myEnumerator.myStorage.putInt(id + myParentValueRefOffset, (Integer)value);
                    return;
                }

                oldValueOffset = readValueId(id);
                updateValueId(id, newValueOffset, oldValueOffset, key, 0);
            }

            if (oldValueOffset != NULL_ADDR) {
                myLiveAndGarbageKeysCounter++;
            }
            else {
                myLiveAndGarbageKeysCounter += LIVE_KEY_MASK;
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    @Override
    public final int enumerate(Key name) throws IOException {
        if (myIsReadOnly) throw new IncorrectOperationException();
        synchronized (myEnumerator) {
            myIntAddressForNewRecord = canUseIntAddressForNewRecord(myValueStorage.getSize());
            return super.enumerate(name);
        }
    }

    /**
     * Appends value chunk from specified appender to key's value.
     * Important use note: value externalizer used by this map should process all bytes from DataInput during deserialization and make sure
     * that deserialized value is consistent with value chunks appended.
     * E.g. Value can be Set of String and individual Strings can be appended with this method for particular key, when {@link #get(Object)} will
     * be eventually called for the key, deserializer will read all bytes retrieving Strings and collecting them into Set
     */
    public final void appendData(Key key, @NotNull PersistentHashMap.ValueDataAppender appender) throws IOException {
        if (myIsReadOnly) throw new IncorrectOperationException();
        synchronized (myEnumerator) {
            try {
                doAppendData(key, appender);
            }
            catch (IOException ex) {
                myEnumerator.markCorrupted();
                throw ex;
            }
        }
    }

    public final void appendDataWithoutCache(Key key, Value value) throws IOException {
        synchronized (myEnumerator) {
            try {
                final BufferExposingByteArrayOutputStream bytes = new BufferExposingByteArrayOutputStream();
                AppendStream appenderStream = ourFlyweightAppenderStream.getValue();
                appenderStream.setOut(bytes);
                myValueExternalizer.save(appenderStream, value);
                appenderStream.setOut(null);
                appendDataWithoutCache(key, bytes);
            }
            catch (IOException ex) {
                markCorrupted();
                throw ex;
            }
        }
    }

    private void appendDataWithoutCache(Key key, @NotNull final BufferExposingByteArrayOutputStream bytes) {
        myEnumerator.lockStorage();
        try {
            long previousRecord;
            final int id;
            if (myDirectlyStoreLongFileOffsetMode) {
                previousRecord = ((PersistentBTreeEnumerator<Key>)myEnumerator).getNonNegativeValue(key);
                id = -1;
            }
            else {
                id = enumerate(key);
                previousRecord = readValueId(id);
            }

            long headerRecord = myValueStorage.appendBytes(bytes.toByteArraySequence(), previousRecord);

            if (myDirectlyStoreLongFileOffsetMode) {
                ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(key, headerRecord);
            }
            else {
                updateValueId(id, headerRecord, previousRecord, key, 0);
            }

            if (previousRecord == NULL_ADDR) {
                myLiveAndGarbageKeysCounter += LIVE_KEY_MASK;
            }

            if (bytes.getInternalBuffer().length <= MAX_RECYCLED_BUFFER_SIZE) {
                // Avoid internal fragmentation by not retaining / reusing large append buffers (IDEA-208533)
                myStreamPool.recycle(bytes);
            }
        }
        catch (IOException e) {
            markCorrupted();
            throw new RuntimeException(e);
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    private static final ThreadLocalCachedValue<AppendStream> ourFlyweightAppenderStream = new ThreadLocalCachedValue<AppendStream>() {
        @NotNull
        @Override
        protected AppendStream create() {
            return new AppendStream();
        }
    };

    private void doAppendData(Key key, @NotNull PersistentHashMap.ValueDataAppender appender) throws IOException {
        assert !myIntMapping;
        myEnumerator.markDirty(true);

        AppendStream appenderStream = ourFlyweightAppenderStream.getValue();
        BufferExposingByteArrayOutputStream stream = myAppendCache.get(key);
        appenderStream.setOut(stream);
        myValueStorage.checkAppendsAllowed(stream.size());
        appender.append(appenderStream);
        appenderStream.setOut(null);
    }

    /**
     * Process all keys registered in the map. Note that keys which were removed after {@link #compact()} call will be processed as well. Use
     * {@link #processKeysWithExistingMapping(Processor)} to process only keys with existing mappings
     */
    @Override
    public final boolean processKeys(@NotNull Processor<? super Key> processor) throws IOException {
        synchronized (myEnumerator) {
            try {
                myAppendCache.clear();
                return myEnumerator.iterateData(processor);
            }
            catch (IOException e) {
                myEnumerator.markCorrupted();
                throw e;
            }
        }
    }

    @NotNull
    public Collection<Key> getAllKeysWithExistingMapping() throws IOException {
        final List<Key> values = new ArrayList<>();
        processKeysWithExistingMapping(new CommonProcessors.CollectProcessor<>(values));
        return values;
    }

    public final boolean processKeysWithExistingMapping(Processor<? super Key> processor) throws IOException {
        synchronized (myEnumerator) {
            try {
                myAppendCache.clear();
                return myEnumerator.processAllDataObject(processor, new PersistentEnumerator.DataFilter() {
                    @Override
                    public boolean accept(final int id) {
                        return readValueId(id) != NULL_ADDR;
                    }
                });
            }
            catch (IOException e) {
                myEnumerator.markCorrupted();
                throw e;
            }
        }
    }

    @Override
    public final Value get(Key key) throws IOException {
        synchronized (myEnumerator) {
            myBusyReading = true;
            try {
                return doGet(key);
            }
            catch (IOException ex) {
                myEnumerator.markCorrupted();
                throw ex;
            }
            finally {
                myBusyReading = false;
            }
        }
    }

    public boolean isBusyReading() {
        return myBusyReading;
    }

    @Nullable
    protected Value doGet(Key key) throws IOException {
        myEnumerator.lockStorage();
        final long valueOffset;
        final int id;
        try {
            myAppendCache.remove(key);

            if (myDirectlyStoreLongFileOffsetMode) {
                valueOffset = ((PersistentBTreeEnumerator<Key>)myEnumerator).getNonNegativeValue(key);
                if (myIntMapping) {
                    return (Value)(Integer)(int)valueOffset;
                }
                id = -1;
            }
            else {
                id = tryEnumerate(key);
                if (id == PersistentEnumeratorBase.NULL_ID) {
                    return null;
                }

                if (myIntMapping) {
                    return (Value)(Integer)myEnumerator.myStorage.getInt(id + myParentValueRefOffset);
                }

                valueOffset = readValueId(id);
            }

            if (valueOffset == NULL_ADDR) {
                return null;
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }

        final PersistentHashMapValueStorage.ReadResult readResult = myValueStorage.readBytes(valueOffset);

        final Value valueRead;
        try (DataInputStream input = new DataInputStream(new UnsyncByteArrayInputStream(readResult.buffer))) {
            valueRead = myValueExternalizer.read(input);
        }

        if (myValueStorage.performChunksCompaction(readResult.chunksCount, readResult.buffer.length)) {

            long newValueOffset = myValueStorage.compactChunks(new PersistentHashMap.ValueDataAppender() {
                @Override
                public void append(DataOutput out) throws IOException {
                    myValueExternalizer.save(out, valueRead);
                }
            }, readResult);

            myEnumerator.lockStorage();
            try {
                myEnumerator.markDirty(true);

                if (myDirectlyStoreLongFileOffsetMode) {
                    ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(key, newValueOffset);
                }
                else {
                    updateValueId(id, newValueOffset, valueOffset, key, 0);
                }
                myLiveAndGarbageKeysCounter++;
                myReadCompactionGarbageSize += readResult.buffer.length;
            }
            finally {
                myEnumerator.unlockStorage();
            }
        }
        return valueRead;
    }

    public final boolean containsMapping(Key key) throws IOException {
        synchronized (myEnumerator) {
            return doContainsMapping(key);
        }
    }

    private boolean doContainsMapping(Key key) throws IOException {
        myEnumerator.lockStorage();
        try {
            myAppendCache.remove(key);
            if (myDirectlyStoreLongFileOffsetMode) {
                return ((PersistentBTreeEnumerator<Key>)myEnumerator).getNonNegativeValue(key) != NULL_ADDR;
            }
            else {
                final int id = tryEnumerate(key);
                if (id == PersistentEnumeratorBase.NULL_ID) {
                    return false;
                }
                if (myIntMapping) return true;
                return readValueId(id) != NULL_ADDR;
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    public final void remove(Key key) throws IOException {
        if (myIsReadOnly) throw new IncorrectOperationException();
        synchronized (myEnumerator) {
            doRemove(key);
        }
    }

    protected void doRemove(Key key) throws IOException {
        myEnumerator.lockStorage();
        try {

            myAppendCache.remove(key);
            final long record;
            if (myDirectlyStoreLongFileOffsetMode) {
                assert !myIntMapping; // removal isn't supported
                record = ((PersistentBTreeEnumerator<Key>)myEnumerator).getNonNegativeValue(key);
                if (record != NULL_ADDR) {
                    ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(key, NULL_ADDR);
                }
            }
            else {
                final int id = tryEnumerate(key);
                if (id == PersistentEnumeratorBase.NULL_ID) {
                    return;
                }
                assert !myIntMapping; // removal isn't supported
                myEnumerator.markDirty(true);

                record = readValueId(id);
                updateValueId(id, NULL_ADDR, record, key, 0);
            }
            if (record != NULL_ADDR) {
                myLiveAndGarbageKeysCounter++;
                myLiveAndGarbageKeysCounter -= LIVE_KEY_MASK;
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    @Override
    public final void force() {
        if (myIsReadOnly) return;
        if (myDoTrace) LOG.info("Forcing " + myStorageFile);
        synchronized (myEnumerator) {
            doForce();
        }
    }

    protected void doForce() {
        myEnumerator.lockStorage();
        try {
            try {
                clearAppenderCaches();
            }
            finally {
                super.force();
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    private void clearAppenderCaches() {
        myAppendCache.clear();
        myValueStorage.force();
    }

    @Override
    public final void close() throws IOException {
        if (myDoTrace) LOG.info("Closed " + myStorageFile);
        synchronized (myEnumerator) {
            doClose();
        }
    }

    private void doClose() throws IOException {
        myEnumerator.lockStorage();
        try {
            try {
                myAppendCacheFlusher.stop();
                try {
                    myAppendCache.clear();
                }
                catch (RuntimeException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof IOException) throw (IOException)cause;
                    throw ex;
                }
            }
            finally {
                final PersistentHashMapValueStorage valueStorage = myValueStorage;
                try {
                    if (valueStorage != null) {
                        valueStorage.dispose();
                    }
                }
                finally {
                    super.close();
                }
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
    }

    //static class CompactionRecordInfo {
    //    final int key;
    //    final int address;
    //    long valueAddress;
    //    long newValueAddress;
    //    byte[] value;
    //
    //    CompactionRecordInfo(int _key, long _valueAddress, int _address) {
    //        key = _key;
    //        address = _address;
    //        valueAddress = _valueAddress;
    //    }
    //}

    // made public for tests
    public void compact() throws IOException {
        if (myIsReadOnly) throw new IncorrectOperationException();
        synchronized (myEnumerator) {
            force();
            LOG.info("Compacting " + myEnumerator.myFile.getPath());
            LOG.info("Live keys:" + (int)(myLiveAndGarbageKeysCounter / LIVE_KEY_MASK) +
                     ", dead keys:" + (int)(myLiveAndGarbageKeysCounter & DEAD_KEY_NUMBER_MASK) +
                     ", read compaction size:" + myReadCompactionGarbageSize);

            final long now = System.currentTimeMillis();

            final File oldDataFile = getDataFile(myEnumerator.myFile);
            final String oldDataFileBaseName = oldDataFile.getName();
            final File[] oldFiles = getFilesInDirectoryWithNameStartingWith(oldDataFile, oldDataFileBaseName);

            final String newPath = getDataFile(myEnumerator.myFile).getPath() + ".new";
            PersistentHashMapValueStorage.CreationTimeOptions options = myValueStorage.getOptions();
            final PersistentHashMapValueStorage newStorage = PersistentHashMapValueStorage.create(newPath, options);
            myValueStorage.switchToCompactionMode();
            myEnumerator.markDirty(true);
            long sizeBefore = myValueStorage.getSize();

            myLiveAndGarbageKeysCounter = 0;
            myReadCompactionGarbageSize = 0;

            try {
                if (doNewCompact()) {
                    newCompact(newStorage);
                }
                else {
                    traverseAllRecords(new PersistentEnumerator.RecordsProcessor() {
                        @Override
                        public boolean process(final int keyId) throws IOException {
                            final long record = readValueId(keyId);
                            if (record != NULL_ADDR) {
                                PersistentHashMapValueStorage.ReadResult readResult = myValueStorage.readBytes(record);
                                long value = newStorage.appendBytes(readResult.buffer, 0, readResult.buffer.length, 0);
                                updateValueId(keyId, value, record, null, getCurrentKey());
                                myLiveAndGarbageKeysCounter += LIVE_KEY_MASK;
                            }
                            return true;
                        }
                    });
                }
            }
            finally {
                newStorage.dispose();
            }

            myValueStorage.dispose();

            if (oldFiles != null) {
                for (File f : oldFiles) {
                    assert FileUtil.deleteWithRenaming(f);
                }
            }

            final long newSize = newStorage.getSize();

            File newDataFile = new File(newPath);
            final String newBaseName = newDataFile.getName();
            final File[] newFiles = getFilesInDirectoryWithNameStartingWith(newDataFile, newBaseName);

            if (newFiles != null) {
                File parentFile = newDataFile.getParentFile();

                // newFiles should get the same names as oldDataFiles
                for (File f : newFiles) {
                    String nameAfterRename = StringUtil.replace(f.getName(), newBaseName, oldDataFileBaseName);
                    FileUtil.rename(f, new File(parentFile, nameAfterRename));
                }
            }

            myValueStorage = PersistentHashMapValueStorage.create(oldDataFile.getPath(), options);
            LOG.info("Compacted " + myEnumerator.myFile.getPath() + ":" + sizeBefore + " bytes into " +
                     newSize + " bytes in " + (System.currentTimeMillis() - now) + "ms.");
            myEnumerator.putMetaData(myLiveAndGarbageKeysCounter);
            myEnumerator.putMetaData2(myLargeIndexWatermarkId);
            if (myDoTrace) LOG.assertTrue(myEnumerator.isDirty());
        }
    }

    private static File[] getFilesInDirectoryWithNameStartingWith(@NotNull File fileFromDirectory, @NotNull final String baseFileName) {
        File parentFile = fileFromDirectory.getParentFile();
        return parentFile != null ? parentFile.listFiles(pathname -> pathname.getName().startsWith(baseFileName)) : null;
    }

    private void newCompact(@NotNull PersistentHashMapValueStorage newStorage) throws IOException {
        long started = System.currentTimeMillis();
        final List<PersistentHashMap.CompactionRecordInfo> infos = new ArrayList<>(10000);

        traverseAllRecords(new PersistentEnumerator.RecordsProcessor() {
            @Override
            public boolean process(final int keyId) {
                final long record = readValueId(keyId);
                if (record != NULL_ADDR) {
                    infos.add(new PersistentHashMap.CompactionRecordInfo(getCurrentKey(), record, keyId));
                }
                return true;
            }
        });

        LOG.info("Loaded mappings:" + (System.currentTimeMillis() - started) + "ms, keys:" + infos.size());
        started = System.currentTimeMillis();
        long fragments = 0;
        if (!infos.isEmpty()) {
            try {
                fragments = myValueStorage.compactValues(infos, newStorage);
            }
            catch (Throwable t) {
                if (!(t instanceof IOException)) throw new IOException("Compaction failed", t);
                throw (IOException)t;
            }
        }

        LOG.info("Compacted values for:" + (System.currentTimeMillis() - started) + "ms fragments:" +
                 (int)fragments + ", new fragments:" + (fragments >> 32));

        started = System.currentTimeMillis();
        try {
            myEnumerator.lockStorage();

            for (PersistentHashMap.CompactionRecordInfo info : infos) {
                updateValueId(info.address, info.newValueAddress, info.valueAddress, null, info.key);
                myLiveAndGarbageKeysCounter += LIVE_KEY_MASK;
            }
        }
        finally {
            myEnumerator.unlockStorage();
        }
        LOG.info("Updated mappings:" + (System.currentTimeMillis() - started) + " ms");
    }

    private long readValueId(final int keyId) {
        if (myDirectlyStoreLongFileOffsetMode) {
            return ((PersistentBTreeEnumerator<Key>)myEnumerator).keyIdToNonNegativeOffset(keyId);
        }
        long address = myEnumerator.myStorage.getInt(keyId + myParentValueRefOffset);
        if (address == 0 || address == -POSITIVE_VALUE_SHIFT) {
            return NULL_ADDR;
        }

        if (address < 0) {
            address = -address - POSITIVE_VALUE_SHIFT;
        }
        else {
            long value = myEnumerator.myStorage.getInt(keyId + myParentValueRefOffset + 4) & 0xFFFFFFFFL;
            address = ((address << 32) + value) & ~USED_LONG_VALUE_MASK;
        }

        return address;
    }

    private int smallKeys;
    private int largeKeys;
    private int transformedKeys;
    private int requests;

    private int updateValueId(int keyId, long value, long oldValue, @Nullable Key key, int processingKey) throws IOException {
        if (myDirectlyStoreLongFileOffsetMode) {
            ((PersistentBTreeEnumerator<Key>)myEnumerator).putNonNegativeValue(((InlineKeyDescriptor<Key>)myKeyDescriptor).fromInt(processingKey), value);
            return keyId;
        }
        final boolean newKey = oldValue == NULL_ADDR;
        if (newKey) ++requests;
        boolean defaultSizeInfo = true;

        if (myCanReEnumerate) {
            if (canUseIntAddressForNewRecord(value)) {
                defaultSizeInfo = false;
                myEnumerator.myStorage.putInt(keyId + myParentValueRefOffset, -(int)(value + POSITIVE_VALUE_SHIFT));
                if (newKey) ++smallKeys;
            }
            else if ((keyId < myLargeIndexWatermarkId || myLargeIndexWatermarkId == 0) && (newKey || canUseIntAddressForNewRecord(oldValue))) {
                // keyId is result of enumerate, if we do re-enumerate then it is no longer accessible unless somebody cached it
                myIntAddressForNewRecord = false;
                keyId = myEnumerator.reEnumerate(key == null ? myEnumerator.getValue(keyId, processingKey) : key);
                ++transformedKeys;
                if (myLargeIndexWatermarkId == 0) {
                    myLargeIndexWatermarkId = keyId;
                }
            }
        }

        if (defaultSizeInfo) {
            value |= USED_LONG_VALUE_MASK;

            myEnumerator.myStorage.putInt(keyId + myParentValueRefOffset, (int)(value >>> 32));
            myEnumerator.myStorage.putInt(keyId + myParentValueRefOffset + 4, (int)value);

            if (newKey) ++largeKeys;
        }

        if (newKey && IOStatistics.DEBUG && (requests & IOStatistics.KEYS_FACTOR_MASK) == 0) {
            IOStatistics.dump("small:" + smallKeys + ", large:" + largeKeys + ", transformed:" + transformedKeys +
                              ",@" + getBaseFile().getPath());
        }
        if (doHardConsistencyChecks) {
            long checkRecord = readValueId(keyId);
            assert checkRecord == (value & ~USED_LONG_VALUE_MASK) : value;
        }
        return keyId;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + myStorageFile;
    }

    @TestOnly
    PersistentHashMapValueStorage getValueStorage() {
        return myValueStorage;
    }

    @TestOnly
    public boolean getReadOnly() {
        return myIsReadOnly;
    }
}
