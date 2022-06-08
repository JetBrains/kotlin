fun box(): String {
    try {
        bar()
        return "FAIL1"
    } catch (e: Throwable) {
        if (!e.isLinkageError()) return "FAIL2"
    }

    return "OK"
}

private fun Throwable.isLinkageError(): Boolean =
    this::class.simpleName == "IrLinkageError" && message == "Unlinked type of IR expression"
