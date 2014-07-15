/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.j2k

import org.jetbrains.jet.j2k.PostProcessor
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.intentions.RemoveExplicitTypeArguments
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import java.util.ArrayList

public object J2kPostProcessor : PostProcessor {
    override fun analyzeFile(file: JetFile): BindingContext {
        return file.getAnalysisResults().getBindingContext()
    }

    override fun doAdditionalProcessing(file: JetFile) {
        val redundantTypeArgs = ArrayList<JetTypeArgumentList>()
        file.accept(object : JetTreeVisitorVoid(){
            override fun visitTypeArgumentList(typeArgumentList: JetTypeArgumentList) {
                if (RemoveExplicitTypeArguments().isApplicableTo(typeArgumentList)) {
                    redundantTypeArgs.add(typeArgumentList)
                    return
                }

                super.visitTypeArgumentList(typeArgumentList)
            }
        })

        for (typeArgs in redundantTypeArgs) {
            typeArgs.delete()
        }
    }
}