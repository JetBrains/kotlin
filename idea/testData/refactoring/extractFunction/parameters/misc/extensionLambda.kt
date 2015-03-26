// PARAM_DESCRIPTOR: local final fun Builder.<anonymous>(): kotlin.Unit defined in test.<anonymous>
// PARAM_TYPES: Builder

class Builder()

fun Builder.build(b: Builder.() -> Unit) {
}

fun Builder.test(b: Builder, param: Int) {
    build {
        build {
            <selection>build {
                build {
                }
            }</selection>
        }
    }
}