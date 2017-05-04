// INTENTION_TEXT: Add Parcelable Implementation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintParcelCreatorInspection
import android.os.Parcel
import android.os.Parcelable

class <caret>MissingCreator : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        TODO("not implemented")
    }

    override fun describeContents(): Int {
        TODO("not implemented")
    }
}