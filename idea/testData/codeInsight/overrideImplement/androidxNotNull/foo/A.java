package foo;

import androidx.annotation.RecentlyNonNull;
import androidx.annotation.RecentlyNullable;

public class A {
    public @RecentlyNonNull String foo(@RecentlyNonNull String s1, @RecentlyNullable String s2) {
        return null;
    }
}
