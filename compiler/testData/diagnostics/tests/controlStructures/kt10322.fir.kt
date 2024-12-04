// RUN_PIPELINE_TILL: BACKEND
fun test1() {
    run {
        if (true) {
            if (true) {}
        }
        else {
            1
        }
    }
}
