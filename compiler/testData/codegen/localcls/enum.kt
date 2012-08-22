fun box(): String {
    enum class K {
        O
        K
    }

    return K.O.toString() + K.K.toString()
}
