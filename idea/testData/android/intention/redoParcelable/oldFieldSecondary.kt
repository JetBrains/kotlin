// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.RedoParcelableAction
import android.os.Parcel
import android.os.Parcelable

class <caret>SomeData(val number: Int, val text: String, val flag: Boolean) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()) {
        someOtherField = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(oldField)
        parcel.writeInt(someOtherField)
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