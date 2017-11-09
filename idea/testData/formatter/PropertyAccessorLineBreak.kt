class A {
    override val location: Location get() =
    if (team != null)
        team.location(organization)
    else
        organization.location().teams

    val name get() = "a"

    /** The age. */
    val age get() = 1
}