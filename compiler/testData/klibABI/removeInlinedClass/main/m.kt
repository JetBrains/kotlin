fun box(): String {
    try {
        fooVariableType()
        return "FAIL1"
    } catch (e: Throwable) {
        if (!e.isUnlinkedTypeOfExpression()) return "FAIL2"
    }

    try {
        barVariableType()
        return "FAIL3"
    } catch (e: Throwable) {
        if (!e.isUnlinkedTypeOfExpression()) return "FAIL4"
    }

    try {
        fooInstance()
        return "FAIL5"
    } catch(e: Throwable) {
        if (!e.isUnlinkedSymbolLinkageError("/Foo.<init>")) return "FAIL6"
    }

    try {
        barInstance()
        return "FAIL7"
    } catch(e: Throwable) {
        if (!e.isUnlinkedTypeInSignature("/Bar.<init>")) return "FAIL8"
    }

    try {
        fooInstance2()
        return "FAIL9"
    } catch(e: Throwable) {
        if (!e.isUnlinkedSymbolLinkageError("/Foo.<init>")) return "FAIL10"
    }

    try {
        barInstance2()
        return "FAIL11"
    } catch(e: Throwable) {
        if (!e.isUnlinkedTypeInSignature("/Bar.<init>")) return "FAIL12"
    }

    try {
        fooAnonymousObject()
        return "FAIL13"
    } catch(e: Throwable) {
        if (!e.isUnlinkedTypeOfExpression()) return "FAIL14"
    }

    return "OK"
}

private fun Throwable.isUnlinkedTypeOfExpression(): Boolean =
    this::class.simpleName == "IrLinkageError" && message == "Unlinked type of IR expression"

private fun Throwable.isUnlinkedSymbolLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true

private fun Throwable.isUnlinkedTypeInSignature(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked type in signature of IR symbol $symbolName|") == true
