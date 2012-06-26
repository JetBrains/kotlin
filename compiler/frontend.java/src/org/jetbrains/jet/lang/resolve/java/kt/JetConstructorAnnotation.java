/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.utils.BitSetUtils;

import java.util.BitSet;

/**
 * @author Stepan Koltsov
 */
public class JetConstructorAnnotation extends PsiAnnotationWithFlags {

    public JetConstructorAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    private boolean hidden;
    private boolean hiddenInitialized = false;
    /** @deprecated */
    public boolean hidden() {
        if (!hiddenInitialized) {
            hidden = getBooleanAttribute(JvmStdlibNames.JET_CONSTRUCTOR_HIDDEN_FIELD, false);
            hiddenInitialized = true;
        }
        return hidden;
    }

    private BitSet flags;
    @NotNull
    @Override
    public BitSet flags() {
        if (flags == null) {
            flags = BitSetUtils.toBitSet(getIntAttribute(JvmStdlibNames.JET_CONSTRUCTOR_FLAGS_FIELD, JvmStdlibNames.FLAGS_DEFAULT_VALUE));
        }
        return flags;
    }
    
    public static JetConstructorAnnotation get(PsiMethod constructor) {
        return new JetConstructorAnnotation(JavaDescriptorResolver.findAnnotation(constructor, JvmStdlibNames.JET_CONSTRUCTOR.getFqName().getFqName()));
    }
}
