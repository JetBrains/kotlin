// RUN_PIPELINE_TILL: BACKEND
val x: Int? = 0
    get() {
        if (field != null) return field.hashCode()
        return null
    }
