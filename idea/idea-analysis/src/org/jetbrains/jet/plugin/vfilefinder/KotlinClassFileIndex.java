/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.vfilefinder;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;

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
            return file.getFileType() == JavaClassFileType.INSTANCE;
        }
    };
    public static final DataIndexer<FqName, Void, FileContent> INDEXER = new DataIndexer<FqName, Void, FileContent>() {
        @NotNull
        @Override
        public Map<FqName, Void> map(FileContent inputData) {
            try {
                KotlinJvmBinaryClass kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(inputData.getFile());
                if (kotlinClass != null && kotlinClass.getClassHeader().getKind() != KotlinClassHeader.Kind.INCOMPATIBLE_ABI_VERSION) {
                    return Collections.singletonMap(kotlinClass.getClassName().getFqNameForClassNameWithoutDollars(), null);
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
