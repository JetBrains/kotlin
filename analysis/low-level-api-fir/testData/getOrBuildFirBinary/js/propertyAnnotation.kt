// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

@Y
var prop: Int = 0
    @Y get() = field
    @Y set(value) { field = value }

annotation class Y
