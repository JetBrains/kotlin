fun useAInAppMain(a: A) {
    println(a.x)
}

typealias ExportedAlias = String

/** A declaration generated from klib metadata. */
@JsExport
fun richDts(value: ExportedAlias): ExportedAlias = value
