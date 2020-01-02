package sample
fun common(): Boolean {
    return <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but Boolean was expected">""</error>
}