package a

import a.impl.KImpl

open class K {
    companion object {
        fun getInstance(): KImpl = KImpl()
    }
}
