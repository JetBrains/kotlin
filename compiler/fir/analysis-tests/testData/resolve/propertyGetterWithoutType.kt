// ISSUE: KT-59649
val prop
    get() {
        fun smth(s: String) = 1
        return smth("awd")
    }
