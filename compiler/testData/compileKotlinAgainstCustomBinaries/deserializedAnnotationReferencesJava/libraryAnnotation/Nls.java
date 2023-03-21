import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE, ElementType.TYPE, ElementType.PACKAGE})
public @interface Nls {
    enum Capitalization {

        NotSpecified,
        /**
         * e.g. This Is a Title
         */
        Title,
        /**
         * e.g. This is a sentence
         */
        Sentence
    }

    Capitalization capitalization() default Capitalization.NotSpecified;
}
