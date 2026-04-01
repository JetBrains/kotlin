if (true) {
    /* ClassId: null [KotlinIllegalArgumentExceptionWithAttachments: Error while resolving org.jetbrains.kotlin.fir.declarations.impl.FirReplSnippetImpl
from RAW_FIR to ANNOTATION_ARGUMENTS
current declaration phase RAW_FIR
origin: Source
session: class org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirScriptSession
module data: class org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
KaModule: class org.jetbrains.kotlin.analysis.test.framework.projectStructure.KaScriptModuleImpl
platform: JVM (1.8)
] */class A
}

fun bar() {
    /* ClassId: null [KotlinIllegalArgumentExceptionWithAttachments: Error while resolving org.jetbrains.kotlin.fir.declarations.impl.FirNamedFunctionImpl
from RAW_FIR to ANNOTATION_ARGUMENTS
current declaration phase RAW_FIR
origin: Source
session: class org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirScriptSession
module data: class org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
KaModule: class org.jetbrains.kotlin.analysis.test.framework.projectStructure.KaScriptModuleImpl
platform: JVM (1.8)
] */class Local
}

// IGNORE_CONSISTENCY_CHECK: repl is not properly supported yet