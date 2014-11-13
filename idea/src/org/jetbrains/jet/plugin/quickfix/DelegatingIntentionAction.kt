package org.jetbrains.jet.plugin.quickfix

import com.intellij.codeInsight.intention.IntentionAction

public open class DelegatingIntentionAction(val delegate: IntentionAction): IntentionAction by delegate
