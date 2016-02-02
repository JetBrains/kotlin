import a.OK

fun box(): String {
    val okRef = ::OK

    // TODO
//    val annotations = okRef.annotations
//    val numAnnotations = annotations.size
//    if (numAnnotations != 1) {
//        return "Failed, annotations: $annotations"
//    }

    return okRef.get()
}