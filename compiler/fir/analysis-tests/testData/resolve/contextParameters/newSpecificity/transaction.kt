// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP

interface Transaction

fun transaction(block: context(Transaction) () -> Unit) { }
context(t: Transaction) fun transaction(block: context(Transaction) () -> Unit) { }

fun example() {
    transaction {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>transaction<!> {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>transaction<!> {

            }
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, interfaceDeclaration,
lambdaLiteral, typeWithContext */
