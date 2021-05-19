fun <K, V> intercept(block: (<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'type usage'">@A</error> K, (K) -> V) -> V) {

}

annotation class A
