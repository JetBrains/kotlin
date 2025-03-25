/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing;

import com.intellij.lang.LighterASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.util.diff.FlyweightCapableTreeStructure;

public class KotlinLightParser {
    public static FlyweightCapableTreeStructure<LighterASTNode> parse(PsiBuilder builder) {
        KotlinParsing ktParsing = KotlinParsing.createForTopLevelNonLazy(new SemanticWhitespaceAwarePsiBuilderImpl(builder));
        ktParsing.parseFile();

        return builder.getLightTree();
    }
}
