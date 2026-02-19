import org.gradle.api.Describable
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.util.Properties

internal fun ProviderFactory.localProperties(
    rootDir: File
): Provider<Map<String, String>> =
    of(CustomPropertiesFileValueSource::class.java) {
        it.parameters.propertiesFile.set(
            rootDir.resolve("local.properties")
        )
    }

internal abstract class CustomPropertiesFileValueSource : ValueSource<Map<String, String>, CustomPropertiesFileValueSource.Parameters>,
    Describable {

    interface Parameters : ValueSourceParameters {
        val propertiesFile: RegularFileProperty
    }

    override fun getDisplayName(): String = "properties file ${parameters.propertiesFile.get().asFile.absolutePath}"

    override fun obtain(): Map<String, String> {
        val customFile = parameters.propertiesFile.get().asFile
        return if (customFile.exists()) {
            customFile.bufferedReader().use {
                @Suppress("UNCHECKED_CAST")
                Properties().apply { load(it) }.toMap() as Map<String, String>
            }
        } else {
            emptyMap()
        }
    }
}
