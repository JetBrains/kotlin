// RUN_PIPELINE_TILL: FRONTEND
interface I
open class C

error class E1 : <!ERROR_CLASS_HAS_SUPERTYPE!>I<!>
error class E2 : <!ERROR_CLASS_HAS_SUPERTYPE!>C<!>()
error class E3 : <!ERROR_CLASS_HAS_SUPERTYPE!>Any<!>()

error class E4<<!ERROR_CLASS_HAS_TYPE_PARAMETER!>T<!>>
error class E5<<!ERROR_CLASS_HAS_TYPE_PARAMETER!>T<!>, <!ERROR_CLASS_HAS_TYPE_PARAMETER!>R : Any<!>>

error object O1 : <!ERROR_CLASS_HAS_SUPERTYPE!>I<!>
error object O2 : <!ERROR_CLASS_HAS_SUPERTYPE!>C<!>()
error object O3 : <!ERROR_CLASS_HAS_SUPERTYPE!>Any<!>()

typealias RE = RichError

error class E6 : RE()

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, objectDeclaration */
