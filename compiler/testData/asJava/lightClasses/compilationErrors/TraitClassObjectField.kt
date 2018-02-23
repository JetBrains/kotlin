// TraitClassObjectField

interface TraitClassObjectField {
    companion object {
        const val x: String? = ""
        private val y: String? = ""
            // Custom getter is needed, otherwise no need to generate getY
            get() = field
    }
}