{
    var classes = function () {
        var HelloKotlin = Kotlin.Class.create({initialize:function () {
            this.$relX = 0.2 + 0.2 * Math.random();
            this.$relY = 0.4 + 0.2 * Math.random();
            this.$relXVelocity = this.randomVelocity();
            this.$relYVelocity = this.randomVelocity();
            this.$message = 'Hello, Kotlin!';
            this.$textHeightInPixels = 60;
            {
                hello.get_context().font = 'bold ' + this.get_textHeightInPixels() + 'px Georgia, serif';
            }
            this.$textWidthInPixels = hello.get_context().measureText(this.get_message()).width;
        }, get_relX:function () {
            return this.$relX;
        }, set_relX:function (tmp$0) {
            this.$relX = tmp$0;
        }, get_relY:function () {
            return this.$relY;
        }, set_relY:function (tmp$0) {
            this.$relY = tmp$0;
        }, get_absX:function () {
            {
                return this.get_relX() * hello.get_width();
            }
        }, get_absY:function () {
            {
                return this.get_relY() * hello.get_height();
            }
        }, get_relXVelocity:function () {
            return this.$relXVelocity;
        }, set_relXVelocity:function (tmp$0) {
            this.$relXVelocity = tmp$0;
        }, get_relYVelocity:function () {
            return this.$relYVelocity;
        }, set_relYVelocity:function (tmp$0) {
            this.$relYVelocity = tmp$0;
        }, get_message:function () {
            return this.$message;
        }, get_textHeightInPixels:function () {
            return this.$textHeightInPixels;
        }, get_textWidthInPixels:function () {
            return this.$textWidthInPixels;
        }, draw:function () {
            {
                hello.get_context().save();
                this.move();
                hello.get_context().shadowColor = '#000000';
                hello.get_context().shadowBlur = 5;
                hello.get_context().shadowOffsetX = -4;
                hello.get_context().shadowOffsetY = 4;
                hello.get_context().fillStyle = 'rgb(242,160,110)';
                hello.get_context().fillText(this.get_message(), this.get_absX(), this.get_absY());
                hello.get_context().restore();
            }
        }, move:function () {
            {
                var relTextWidth = this.get_textWidthInPixels() / hello.get_width();
                if (this.get_relX() > 1 - relTextWidth - this.get_abs(this.get_relXVelocity()) || this.get_relX() < this.get_abs(this.get_relXVelocity())) {
                    this.set_relXVelocity(this.get_relXVelocity() * -1);
                }
                var relTextHeight = this.get_textHeightInPixels() / hello.get_height();
                if (this.get_relY() > 1 - this.get_abs(this.get_relYVelocity()) || this.get_relY() < this.get_abs(this.get_relYVelocity()) + relTextHeight) {
                    this.set_relYVelocity(this.get_relYVelocity() * -1);
                }
                this.set_relX(this.get_relX() + this.get_relXVelocity());
                this.set_relY(this.get_relY() + this.get_relYVelocity());
            }
        }, randomVelocity:function () {
            var tmp$0;
            if (Math.random() < 0.5)
                tmp$0 = 1;
            else
                tmp$0 = -1;
            {
                return 0.03 * Math.random() * tmp$0;
            }
        }, get_abs:function (receiver) {
            var tmp$0;
            if (receiver > 0)
                tmp$0 = receiver;
            else
                tmp$0 = -receiver;
            {
                return tmp$0;
            }
        }
        });
        return {HelloKotlin_0:HelloKotlin};
    }
        ();
    var hello = Kotlin.Namespace.create({initialize:function () {
        hello.$context = getContext();
        hello.$height = getCanvas().height;
        hello.$width = getCanvas().width;
    }, get_context:function () {
        return hello.$context;
    }, get_height:function () {
        return hello.$height;
    }, get_width:function () {
        return hello.$width;
    }, renderBackground:function () {
        {
            hello.get_context().save();
            hello.get_context().fillStyle = '#5C7EED';
            hello.get_context().fillRect(0, 0, hello.get_width(), hello.get_height());
            hello.get_context().restore();
        }
    }, main:function () {
        {
            var interval = 50;
            var logos = Kotlin.arrayFromFun(3, function (it) {
                    {
                        return new hello.HelloKotlin_0;
                    }
                }
            );
            $(function () {
                    {
                        setInterval(function () {
                                {
                                    var tmp$0;
                                    hello.renderBackground();
                                    {
                                        tmp$0 = Kotlin.arrayIterator(logos);
                                        while (tmp$0.hasNext()) {
                                            var logo = tmp$0.next();
                                            {
                                                logo.draw();
                                            }
                                        }
                                    }
                                }
                            }
                            , interval);
                    }
                }
            );
        }
    }
    }, classes);
    hello.initialize();
}

var args = [];
hello.main(args);
