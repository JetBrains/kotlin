fun testOpenParens() {
    try {
    } catch {
    }

    try {
    } catch ( {
    }

    try {
    } catch (e: {
    }

    try {
    } catch (e: Exception {
    }
}

fun testClosedParens() {
    try {
    } catch () {
    }

    try {
    } catch (e) {
    }

    try {
    } catch (e:) {
    }

    try {
    } catch (e: Exception) {
    }

    try {
    } catch (: Exception) {
    }

    try {
    } catch (:) {
    }

}

fun testFinally() {
    try {
    } catch
    finally {
    }

    try {
    } catch ()
    finally {
    }

    try {
    } catch (e : Exception)
    finally {
    }
}