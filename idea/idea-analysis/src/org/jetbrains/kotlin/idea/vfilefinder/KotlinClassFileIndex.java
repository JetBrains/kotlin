/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.vfilefinder;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.kotlin.name.FqName;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public final class KotlinClassFileIndex extends ScalarIndexExtension<FqName> {

    private static final Logger LOG = Logger.getInstance(KotlinClassFileIndex.class);
    private static final int VERSION = 2;
    public static final ID<FqName, Void> KEY = ID.create(KotlinClassFileIndex.class.getCanonicalName());

    private static final KeyDescriptor<FqName> KEY_DESCRIPTOR = new KeyDescriptor<FqName>() {
        @Override
        public void save(@NotNull DataOutput out, FqName value) throws IOException {
            out.writeUTF(value.asString());
        }

        @Override
        public FqName read(@NotNull DataInput in) throws IOException {
            return new FqName(in.readUTF());
        }

        @Override
        public int getHashCode(FqName value) {
            return value.asString().hashCode();
        }

        @Override
        public boolean isEqual(FqName val1, FqName val2) {
            return val1 == null ? val2 == null : val1.equals(val2);
        }
    };

    private static final FileBasedIndex.InputFilter INPUT_FILTER = new FileBasedIndex.InputFilter() {
        @Override
        public boolean acceptInput(@NotNull VirtualFile file) {
            return file.getFileType() == JavaClassFileType.INSTANCE;
        }
    };
    public static final DataIndexer<FqName, Void, FileContent> INDEXER = new DataIndexer<FqName, Void, FileContent>() {
        @NotNull
        @Override
        public Map<FqName, Void> map(@NotNull FileContent inputData) {
            try {
                KotlinJvmBinaryClass kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(inputData.getFile());
                if (kotlinClass != null && kotlinClass.getClassHeader().getIsCompatibleAbiVersion()) {
                    return Collections.singletonMap(kotlinClass.getClassId().asSingleFqName(), null);
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

    @NotNull
    @Override
    public KeyDescriptor<FqName> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @NotNull
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
