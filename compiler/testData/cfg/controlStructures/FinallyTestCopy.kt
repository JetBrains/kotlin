fun doSmth() {}
fun doSmth1() {}
fun doSmth2() {}
fun cond() {}

fun testCopy1() : Int {
    try {
        doSmth()
    }
    catch (e: NullPointerException) {
        doSmth1()
    }
    catch (e: Exception) {
        doSmth2()
    }
    finally {
        return 1
    }
}

fun testCopy2() {
    while (cond()) {
        try {
            doSmth()
        }
        catch (e: NullPointerException) {
            doSmth1()
        }
        catch (e: Exception) {
            doSmth2()
        }
        finally {
            if (cond()) return
            else continue
        }
    }
}

fun testCopy3() {
    try {
        doSmth()
    }
    catch (e: NullPointerException) {
        doSmth1()
    }
    catch (e: Exception) {
        doSmth2()
    }
    finally {
        while (cond());
    }
}

fun doTestCopy4(list: List<String>?) : Int {
    try {
        doSmth()
    }
    finally {
        if(list != null) {
        }
    }
}
