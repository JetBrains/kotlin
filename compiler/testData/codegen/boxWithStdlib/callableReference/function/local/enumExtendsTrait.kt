fun box(): String {
    trait Named {
        fun name(): String
    }
    
    enum class E : Named {
        OK
    }

    return E.OK.(Named::name)()
}
