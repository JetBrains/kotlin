val x: Int? = 0
    get() {
        if (field != null) return field.hashCode()
        return null
    }
