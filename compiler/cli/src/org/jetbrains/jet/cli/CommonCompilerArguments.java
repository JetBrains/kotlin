package org.jetbrains.jet.cli;

import com.sampullara.cli.Argument;
import org.jetbrains.jet.cli.common.CompilerArguments;

public abstract class CommonCompilerArguments extends CompilerArguments {
    @Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
    public boolean tags;
    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;
    @Argument(value = "version", description = "Display compiler version")
    public boolean version;
    @Argument(value = "help", alias = "h", description = "Show help")
    public boolean help;

    @Override
    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    @Override
    public boolean isTags() {
        return tags;
    }

    @Override
    public boolean isVersion() {
        return version;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    public void setTags(boolean tags) {
        this.tags = tags;
    }
}
