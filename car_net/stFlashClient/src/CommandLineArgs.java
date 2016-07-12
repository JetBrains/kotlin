import com.beust.jcommander.Parameter;

/**
 * Created by user on 7/12/16.
 */
public class CommandLineArgs {

    @Parameter(names = { "-log", "-verbose" }, description = "Level of verbosity")
    public Integer verbose = 1;

    @Parameter(names = "-groups", description = "Comma-separated list of group names to be run")
    public String groups;

    @Parameter(names = "-debug", description = "Debug mode")
    public boolean debug = false;


}
