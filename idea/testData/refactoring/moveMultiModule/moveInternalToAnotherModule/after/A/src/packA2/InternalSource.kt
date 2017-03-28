package packA2

import packA1.InternalContent

class InternalContentUser {
    fun useInternal(p: InternalContent) = p.internalFun()

    internal fun internalFun() {

    }
}