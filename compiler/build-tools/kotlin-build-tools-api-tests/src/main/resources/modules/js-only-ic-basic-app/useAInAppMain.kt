fun useAInAppMain(a: A) {
    println(a.x)
}

typealias ExportedAlias = String

/** A declaration generated from KLIB metadata. */
@JsExport
fun richDts(value: ExportedAlias): ExportedAlias = value
