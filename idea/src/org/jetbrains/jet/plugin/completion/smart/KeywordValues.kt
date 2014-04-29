package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.codeInsight.lookup.LookupElementBuilder

object KeywordValues {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        if (expectedInfos.any { it.`type` == KotlinBuiltIns.getInstance().getBooleanType() }) {
            collection.add(LookupElementBuilder.create("true").bold())
            collection.add(LookupElementBuilder.create("false").bold())
        }
    }
}