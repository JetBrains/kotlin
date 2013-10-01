package org.jetbrains.jet.plugin.vfilefinder;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassFileHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.SerializedDataHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public final class KotlinClassFileIndex extends ScalarIndexExtension<FqName> {

    private static final Logger LOG = Logger.getInstance(KotlinClassFileIndex.class);
    private static final int VERSION = 1;
    public static final ID<FqName, Void> KEY = ID.create(KotlinClassFileIndex.class.getCanonicalName());

    private static final KeyDescriptor<FqName> KEY_DESCRIPTOR = new KeyDescriptor<FqName>() {
        @Override
        public void save(DataOutput out, FqName value) throws IOException {
            out.writeUTF(value.asString());
        }

        @Override
        public FqName read(DataInput in) throws IOException {
            return new FqName(in.readUTF());
        }

        @Override
        public int getHashCode(FqName value) {
            return value.asString().hashCode();
        }

        @Override
        public boolean isEqual(FqName val1, FqName val2) {
            if (val1 == null) {
                return val2 == null;
            }
            return val1.equals(val1);
        }
    };

    private static final FileBasedIndex.InputFilter INPUT_FILTER = new FileBasedIndex.InputFilter() {
        @Override
        public boolean acceptInput(VirtualFile file) {
            return file.getFileType() == StdFileTypes.CLASS;
        }
    };
    public static final DataIndexer<FqName, Void, FileContent> INDEXER = new DataIndexer<FqName, Void, FileContent>() {
        @NotNull
        @Override
        public Map<FqName, Void> map(FileContent inputData) {
            try {
                KotlinClassFileHeader header = KotlinClassFileHeader.readKotlinHeaderFromClassFile(inputData.getFile());
                if (header instanceof SerializedDataHeader && header.isCompatibleKotlinCompiledFile()) {
                    return Collections.singletonMap(header.getFqName(), null);
                }
            }
            catch (Throwable e) {
                LOG.warn("Error while indexing file " + inputData.getFileName(), e);
            }
            return Collections.emptyMap();
        }
    };

    @NotNull
    @Override
    public ID<FqName, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<FqName, Void, FileContent> getIndexer() {
        return INDEXER;
    }

    @Override
    public KeyDescriptor<FqName> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
