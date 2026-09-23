// FOREIGN_ANNOTATIONS

// FILE: MyNullable.java

import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.NEVER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyNullable {

}

// FILE: NonNullNever.java

import javax.annotation.*;
        import javax.annotation.meta.When;

public class NonNullNever {
    @Nonnull(when = When.NEVER) public String field = null;

    @MyNullable
    public String foo(@Nonnull(when = When.NEVER) String x, @MyNullable CharSequence y) {
        return "";
    }
}
