// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.regex.Pattern

val strs: List<String> = listOf("regex1", "regex2")

val patterns: List<Pattern> = strs.map(Pattern::compile)

/* GENERATED_FIR_TAGS: flexibleType, javaCallableReference, propertyDeclaration, stringLiteral */
