package org.jetbrains.jet.lang.resolve.android

trait AndroidResource

class AndroidID(val rawID: String): AndroidResource {

    override fun equals(other: Any?): Boolean {
        return other is AndroidID && this.rawID == other.rawID
    }
    override fun hashCode(): Int {
        return rawID.hashCode()
    }
    override fun toString(): String {
        return rawID
    }
}

class AndroidWidget(val id: String, val className: String): AndroidResource
