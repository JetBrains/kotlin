
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.meta.TypeQualifierDefault;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@MyMigrationNonnull
@TypeQualifierDefault({ ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
public @interface MyNonnullApi {
}