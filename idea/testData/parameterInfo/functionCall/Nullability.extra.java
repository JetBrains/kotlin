import org.jetbrains.annotations.Nullable
import org.jetbrains.annotations.NotNull
import androidx.annotation.RecentlyNonNull;
import androidx.annotation.RecentlyNullable;

class J {
    static void foo(@Nullable String s1, @NotNull String s2, @RecentlyNullable String a, @RecentlyNonNull String b, String s3) {

    }
}