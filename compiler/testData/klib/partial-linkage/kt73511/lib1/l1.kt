import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Target(CLASS)
@Retention(BINARY)
public annotation class MyAnnotationMarker(
    val markerClass: KClass<out Annotation>
)
