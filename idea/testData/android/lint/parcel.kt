// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintParcelCreatorInspection

@file:Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
import android.os.Parcel
import android.os.Parcelable

class <error descr="This class implements `Parcelable` but does not provide a `CREATOR` field">MyParcelable1</error> : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}
}

internal class MyParcelable2 : Parcelable {
    override fun describeContents() = 0

    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<String> = object : Parcelable.Creator<String> {
            override fun newArray(size: Int) = null!!
            override fun createFromParcel(source: Parcel?) = null!!
        }
    }
}

internal class MyParcelable3 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        @JvmField
        val CREATOR = 0 // Wrong type
    }
}

class RecyclerViewScrollPosition(val position: Int, val topOffset: Int): Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(position)
        dest.writeInt(topOffset)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<RecyclerViewScrollPosition> {
            override fun createFromParcel(parcel: Parcel): RecyclerViewScrollPosition {
                val position = parcel.readInt()
                val topOffset = parcel.readInt()
                return RecyclerViewScrollPosition(position, topOffset)
            }

            override fun newArray(size: Int): Array<RecyclerViewScrollPosition?> = arrayOfNulls(size)
        }

    }
}

class RecyclerViewScrollPositionWithoutJvmF(val position: Int, val topOffset: Int): Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(position)
        dest.writeInt(topOffset)
    }

    companion object {
        val CREATOR = object : Parcelable.Creator<RecyclerViewScrollPosition> {
            override fun createFromParcel(parcel: Parcel): RecyclerViewScrollPosition {
                val position = parcel.readInt()
                val topOffset = parcel.readInt()
                return RecyclerViewScrollPosition(position, topOffset)
            }

            override fun newArray(size: Int): Array<RecyclerViewScrollPosition?> = arrayOfNulls(size)
        }

    }
}

class RecyclerViewScrollPosition2(val position: Int, val topOffset: Int): Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(position)
        dest.writeInt(topOffset)
    }

    companion object CREATOR: Parcelable.Creator<RecyclerViewScrollPosition> {
        override fun createFromParcel(parcel: Parcel): RecyclerViewScrollPosition {
            val position = parcel.readInt()
            val topOffset = parcel.readInt()
            return RecyclerViewScrollPosition(position, topOffset)
        }

        override fun newArray(size: Int): Array<RecyclerViewScrollPosition?> = arrayOfNulls(size)
    }
}

internal abstract class MyParcelable4 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}
}
