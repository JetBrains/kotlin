{
    var classes = function () {
        var Moving = Kotlin.Class.create({initialize:function () {
            this.$context = getContext();
            this.$height = getCanvas().height;
            this.$width = getCanvas().width;
            this.$relX = 0.5;
            this.$relY = 0.5;
            this.$relXVelocity = this.randomVelocity();
            this.$relYVelocity = this.randomVelocity();
            this.$message = 'Hello Kotlin';
            this.$textHeightInPixels = 60;
            {
                this.get_context().font = 'bold ' + this.get_textHeightInPixels() + 'px Georgia, serif';
            }
            this.$textWidthInPixels = this.get_context().measureText(this.get_message()).width;
        }, get_context:function () {
            return this.$context;
        }, get_height:function () {
            return this.$height;
        }, get_width:function () {
            return this.$width;
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
                return this.get_relX() * this.get_width();
            }
        }, get_absY:function () {
            {
                return this.get_relY() * this.get_height();
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
        }, renderText:function () {
            {
                this.get_context().save();
                this.move();
                this.get_context().shadowColor = 'white';
                this.get_context().shadowBlur = 10;
                this.get_context().fillStyle = 'rgba(100,200,0,0.7)';
                this.get_context().fillText(this.get_message(), this.get_absX(), this.get_absY());
                this.get_context().restore();
            }
        }, move:function () {
            {
                var relTextWidth = this.get_textWidthInPixels() / this.get_width();
                if (this.get_relX() > 1 - relTextWidth - this.get_abs(this.get_relXVelocity()) || this.get_relX() < this.get_abs(this.get_relXVelocity())) {
                    this.set_relXVelocity(this.get_relXVelocity() * -1);
                }
                var relTextHeight = this.get_textHeightInPixels() / this.get_height();
                if (this.get_relY() > 1 - this.get_abs(this.get_relYVelocity()) || this.get_relY() < this.get_abs(this.get_relYVelocity()) + relTextHeight) {
                    this.set_relYVelocity(this.get_relYVelocity() * -1);
                }
                this.set_relX(this.get_relX() + this.get_relXVelocity());
                this.set_relY(this.get_relY() + this.get_relYVelocity());
            }
        }, changeDirection:function () {
            {
                this.set_relYVelocity(this.randomVelocity());
                this.set_relXVelocity(this.randomVelocity());
            }
        }, renderBackground:function () {
            {
                this.get_context().save();
                this.get_context().fillStyle = 'rgba(255,255,1,0.2)';
                this.get_context().fillRect(0, 0, this.get_width(), this.get_height());
                this.get_context().restore();
            }
        }, randomVelocity:function () {
            var tmp$0;
            if (Math.random() < 0.5)
                tmp$0 = 1;
            else
                tmp$0 = -1;
            {
                return 0.01 * Math.random() * tmp$0;
            }
        }, run:function () {
            {
                var tmp$1;
                var tmp$0;
                setInterval((tmp$0 = this , function () {
                    {
                        tmp$0.renderBackground();
                        tmp$0.renderText();
                    }
                }
                    ), 10);
                setInterval((tmp$1 = this , function () {
                    {
                        tmp$1.changeDirection();
                    }
                }
                    ), 3000);
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
        return {Moving_0:Moving};
    }
        ();
    var moving = Kotlin.Namespace.create({initialize:function () {
    }, main:function () {
        {
            $(function () {
                    {
                        (new moving.Moving_0).run();
                    }
                }
            );
        }
    }
    }, classes);
    moving.initialize();
}

var args = [];
moving.main(args);
