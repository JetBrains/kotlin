// RUN_PIPELINE_TILL: BACKEND
fun calc(x: List<String>?, y: Int?) {
    // Smart cast should work here despite of KT-7204 fixed
    x?.subList(0, y!!)?.get(y) 
}
