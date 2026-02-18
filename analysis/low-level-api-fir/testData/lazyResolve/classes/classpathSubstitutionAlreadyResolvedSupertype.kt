// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-82945
// MODULE: dep
// PRE_RESOLVED_PHASE: SUPER_TYPES
// FILE: base.kt
package example

interface MyBaseInte<caret_preresolved>rface : InterfaceToSubstitute

interface InterfaceToSubstitute : TransitiveInterface

interface TransitiveInterface

// MODULE: main(dep)
// FILE: main.kt
package example

interface InterfaceToSubstitute

interface TransitiveInterface

class MyCl<caret>ass : MyBaseInterface
