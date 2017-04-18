// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.RedoParcelableAction
import android.os.Parcel
import android.os.Parcelable

class <caret>SomeData(parcel: Parcel) : Parcelable {
    var count = 0

    init {
        oldField = parcel.readInt()
        field9 = parcel.readByte() != 0.toByte()
        goodArray = parcel.createTypedArray(SuperParcelable)
        goodList = parcel.createTypedArrayList(SuperParcelable)
        parcelableProperty = parcel.readParcelable(SuperParcelable::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(oldField)
        parcel.writeByte(if (field9) 1 else 0)
        parcel.writeTypedArray(goodArray, flags)
        parcel.writeTypedList(goodList)
        parcel.writeParcelable(parcelableProperty, flags)
    }

    override fun describeContents(): Int {
        return 42
    }

    companion object CREATOR : Parcelable.Creator<SomeData> {
        override fun createFromParcel(parcel: Parcel): SomeData {
            return SomeData(parcel)
        }

        override fun newArray(size: Int): Array<SomeData?> {
            return arrayOfNulls(size)
        }
    }
}