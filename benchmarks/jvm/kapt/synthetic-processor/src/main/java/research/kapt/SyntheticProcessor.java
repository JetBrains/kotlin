package research.kapt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("research.kapt.SyntheticAnno")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class SyntheticProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Claim the annotation but generate nothing — kapt's stub-gen cost is what we want to isolate.
        return true;
    }
}
